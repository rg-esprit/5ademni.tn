<?php

namespace App\Form;

use App\Entity\Job;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\NotBlank;

class JobType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('title', TextType::class, [
                'constraints' => [new NotBlank(['message' => 'Please enter a job title'])],
                'attr' => ['placeholder' => 'e.g. Senior PHP Developer'],
            ])
            ->add('company', TextType::class, [
                'constraints' => [new NotBlank(['message' => 'Please enter a company name'])],
                'attr' => ['placeholder' => 'e.g. Tech Corp'],
            ])
            ->add('location', TextType::class, [
                'constraints' => [new NotBlank(['message' => 'Please enter a location'])],
                'attr' => ['placeholder' => 'e.g. Tunis, Remote'],
            ])
            ->add('category', ChoiceType::class, [
                'choices' => [
                    'IT & Software' => 'IT & Software',
                    'Design & Creative' => 'Design & Creative',
                    'Marketing' => 'Marketing',
                    'Writing & Translation' => 'Writing & Translation',
                    'Sales & Support' => 'Sales & Support',
                    'Other' => 'Other',
                ],
                'constraints' => [new NotBlank(['message' => 'Please select a category'])],
                'placeholder' => 'Choose a category',
            ])
            ->add('jobType', ChoiceType::class, [
                'choices' => [
                    'Full-time' => 'Full-time',
                    'Part-time' => 'Part-time',
                    'Contract' => 'Contract',
                    'Freelance' => 'Freelance',
                    'Internship' => 'Internship',
                ],
                'placeholder' => 'Choose job type',
                'required' => false,
            ])
            ->add('salaryRange', TextType::class, [
                'required' => false,
                'attr' => ['placeholder' => 'e.g. 1500 - 2500 TND / month'],
            ])
            ->add('description', TextareaType::class, [
                'constraints' => [new NotBlank(['message' => 'Please enter a job description'])],
                'attr' => ['rows' => 5, 'placeholder' => 'Describe the job responsibilities...'],
            ])
            ->add('requirements', TextareaType::class, [
                'required' => false,
                'attr' => ['rows' => 5, 'placeholder' => 'List the requirements, skills, etc...'],
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Job::class,
        ]);
    }
}
